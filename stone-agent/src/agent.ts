import { JobContext, WorkerOptions, cli, defineAgent, multimodal } from '@livekit/agents';
import * as openai from '@livekit/agents-plugin-openai';
import { AudioSource, LocalParticipant, RemoteParticipant, Room } from '@livekit/rtc-node';
import { MemoryClient } from './memory';
import { DynamicToolLoader } from './tools';

/**
 * Unified Stone Agent with Dynamic Tool Loading
 * 
 * This single agent handles all interactions, dynamically loading
 * the appropriate MCP tools based on context.
 */
export class StoneAgent {
  private memory: MemoryClient;
  private toolLoader: DynamicToolLoader;
  private currentContext: string = 'general';
  private room: Room;
  private participant: RemoteParticipant;

  constructor(room: Room, participant: RemoteParticipant) {
    this.room = room;
    this.participant = participant;
    this.memory = new MemoryClient();
    this.toolLoader = new DynamicToolLoader();
  }

  /**
   * Determine context from user input and current app
   */
  private async determineContext(input: string, currentApp?: string): Promise<string> {
    // If we know what app they're in, use that context
    if (currentApp) {
      return currentApp.toLowerCase();
    }

    // Otherwise, analyze the input to determine context
    const contexts = {
      listen: /play|music|song|spotify|playlist|skip|pause/i,
      go: /navigate|directions|map|drive|walk|location|where/i,
      connect: /call|text|message|email|contact|phone/i,
      plan: /calendar|event|meeting|schedule|appointment/i,
      think: /note|write|document|remember|notion/i,
      ask: /search|what|how|why|explain|perplexity/i,
      tick: /timer|alarm|stopwatch|clock|time/i,
      set: /setting|configure|preference|2fa|authentication/i,
      fund: /pay|money|wallet|bank|transfer/i,
      reflect: /journal|today|yesterday|log|activity/i,
      task: /mcp|tool|app|install/i,
    };

    for (const [context, pattern] of Object.entries(contexts)) {
      if (pattern.test(input)) {
        return context;
      }
    }

    return 'general';
  }

  /**
   * Process user input with dynamic tool loading
   */
  public async processInput(input: string, currentApp?: string): Promise<string> {
    // Determine context
    const context = await this.determineContext(input, currentApp);
    
    // Load appropriate tools for this context
    const tools = await this.toolLoader.loadToolsForContext(context);
    
    // Store interaction in memory
    await this.memory.storeInteraction({
      input,
      context,
      timestamp: new Date(),
      app: currentApp,
    });

    // Get relevant memory context
    const memoryContext = await this.memory.getRelevantContext(input);

    // Create the prompt with context
    const systemPrompt = this.buildSystemPrompt(context, memoryContext);

    // Process with OpenAI
    const model = new openai.LLM({
      model: 'gpt-4-turbo-preview',
      temperature: 0.7,
    });

    // Execute with loaded tools
    const response = await model.generate({
      system: systemPrompt,
      messages: [{ role: 'user', content: input }],
      tools: tools,
    });

    return response.content;
  }

  /**
   * Build dynamic system prompt based on context
   */
  private buildSystemPrompt(context: string, memoryContext: any): string {
    const basePrompt = `You are Stone, a helpful AI assistant integrated into StoneOS.
You have access to control various aspects of the user's phone and help them with tasks.
Be concise, helpful, and natural in your responses.`;

    const contextPrompts: Record<string, string> = {
      listen: `You're helping control Spotify. You can play music, create playlists, skip songs, and control playback.`,
      go: `You're helping with navigation. You can find places, get directions, and help the user navigate.`,
      connect: `You're managing communications. You can make calls, send texts, check emails, and manage contacts.`,
      plan: `You're managing the calendar. You can create events, check schedules, and set reminders.`,
      think: `You're helping with notes. You can create, edit, and organize notes in Notion.`,
      ask: `You're helping with research. Use Perplexity to find information and answer questions.`,
      tick: `You're managing time. You can set timers, alarms, and use the stopwatch.`,
      set: `You're managing settings. You can adjust phone settings and help with 2FA.`,
      fund: `You're near financial apps but cannot directly access financial data for security. Guide the user.`,
      reflect: `You're helping with journaling. You have access to the user's activity logs for today.`,
      task: `You're helping discover and install new MCP tools to extend functionality.`,
      general: `You're ready to help with any task. Determine what the user needs and assist accordingly.`,
    };

    const contextPrompt = contextPrompts[context] || contextPrompts.general;
    
    let fullPrompt = `${basePrompt}\n\nCurrent context: ${context}\n${contextPrompt}`;
    
    if (memoryContext) {
      fullPrompt += `\n\nRelevant memory context:\n${JSON.stringify(memoryContext, null, 2)}`;
    }

    return fullPrompt;
  }

  /**
   * Handle voice interaction (no transcription shown to user)
   */
  public async handleVoiceInteraction(): Promise<void> {
    // This connects to LiveKit for voice processing
    // Transcription happens server-side but is NOT shown to user
    // Only the glowing Stone icon indicates AI is listening/speaking
    
    const vad = await openai.VAD.create();
    const stt = new openai.STT({
      model: 'whisper-1',
    });
    const tts = new openai.TTS({
      model: 'tts-1',
      voice: 'nova',
    });

    // Create voice pipeline (no transcription display)
    const assistant = await multimodal.VoiceAssistant.create({
      vad,
      stt,
      llm: this,
      tts,
      room: this.room,
      participant: this.participant,
    });

    await assistant.start();
  }
}

/**
 * LiveKit Agent Entry Point
 */
export default defineAgent({
  entry: async (ctx: JobContext) => {
    await ctx.connect();
    
    const room = ctx.room;
    const participant = room.remoteParticipants.values().next().value;
    
    if (!participant) {
      console.error('No participant found');
      return;
    }

    const agent = new StoneAgent(room, participant);
    
    // Listen for text messages via data channel
    room.on('dataReceived', async (data: Uint8Array, participant: RemoteParticipant) => {
      const message = new TextDecoder().decode(data);
      const parsed = JSON.parse(message);
      
      if (parsed.type === 'text_input') {
        const response = await agent.processInput(parsed.content, parsed.currentApp);
        
        // Send response back
        const responseData = JSON.stringify({
          type: 'text_response',
          content: response,
        });
        await room.localParticipant.publishData(
          new TextEncoder().encode(responseData),
          { reliable: true }
        );
      } else if (parsed.type === 'start_voice') {
        await agent.handleVoiceInteraction();
      }
    });

    console.log('Stone agent ready');
  },
});

// Run the agent
if (require.main === module) {
  cli.runApp(new WorkerOptions({
    entrypoint: exports.default,
    port: 8080,
  }));
}