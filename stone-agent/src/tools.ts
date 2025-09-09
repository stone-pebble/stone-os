import { MCPClient } from '@modelcontextprotocol/client';

/**
 * Dynamic Tool Loader for Stone Agent
 * 
 * Loads appropriate MCP tools based on the current context/app
 */
export class DynamicToolLoader {
  private mcpClients: Map<string, MCPClient> = new Map();
  private toolCache: Map<string, any[]> = new Map();

  constructor() {
    this.initializeMCPClients();
  }

  /**
   * Initialize MCP client connections
   */
  private async initializeMCPClients() {
    const mcpServers = {
      spotify: { port: 9001, name: 'Spotify Control' },
      maps: { port: 9002, name: 'Google Maps' },
      calendar: { port: 9003, name: 'Calendar Management' },
      notion: { port: 9004, name: 'Notion Notes' },
      telephony: { port: 9005, name: 'Phone & SMS' },
      perplexity: { port: 9006, name: 'Perplexity Search' },
      settings: { port: 9007, name: 'System Settings' },
      clock: { port: 9008, name: 'Clock & Timers' },
    };

    for (const [key, config] of Object.entries(mcpServers)) {
      try {
        const client = new MCPClient({
          name: config.name,
          endpoint: `http://localhost:${config.port}`,
        });
        await client.connect();
        this.mcpClients.set(key, client);
        console.log(`Connected to MCP: ${config.name}`);
      } catch (error) {
        console.error(`Failed to connect to ${config.name}:`, error);
      }
    }
  }

  /**
   * Load tools for a specific context
   */
  public async loadToolsForContext(context: string): Promise<any[]> {
    // Check cache first
    if (this.toolCache.has(context)) {
      return this.toolCache.get(context)!;
    }

    const tools: any[] = [];

    // Map contexts to MCP servers
    const contextToMCP: Record<string, string[]> = {
      listen: ['spotify'],
      go: ['maps'],
      connect: ['telephony'],
      plan: ['calendar'],
      think: ['notion'],
      ask: ['perplexity'],
      tick: ['clock'],
      set: ['settings'],
      fund: [], // No tools for financial apps (security)
      reflect: ['calendar', 'telephony'], // Access to activity data
      task: [], // MCP discovery doesn't need tools
      general: ['perplexity', 'calendar', 'telephony'], // Basic tools
    };

    const mcpKeys = contextToMCP[context] || contextToMCP.general;

    for (const mcpKey of mcpKeys) {
      const client = this.mcpClients.get(mcpKey);
      if (client) {
        const mcpTools = await client.listTools();
        tools.push(...this.wrapMCPTools(mcpTools, mcpKey));
      }
    }

    // Add base tools that are always available
    tools.push(...this.getBaseTools());

    // Cache the loaded tools
    this.toolCache.set(context, tools);

    return tools;
  }

  /**
   * Wrap MCP tools for use with the LLM
   */
  private wrapMCPTools(mcpTools: any[], source: string): any[] {
    return mcpTools.map(tool => ({
      name: `${source}_${tool.name}`,
      description: tool.description,
      parameters: tool.inputSchema,
      execute: async (params: any) => {
        const client = this.mcpClients.get(source);
        if (!client) {
          throw new Error(`MCP client ${source} not found`);
        }
        return await client.callTool(tool.name, params);
      },
    }));
  }

  /**
   * Get base tools that are always available
   */
  private getBaseTools(): any[] {
    return [
      {
        name: 'get_current_time',
        description: 'Get the current time',
        parameters: {},
        execute: async () => {
          return new Date().toLocaleString();
        },
      },
      {
        name: 'get_device_info',
        description: 'Get basic device information',
        parameters: {},
        execute: async () => {
          return {
            os: 'StoneOS',
            version: '0.1.0',
            device: 'Pixel 8a',
          };
        },
      },
    ];
  }

  /**
   * Refresh tool cache for a context
   */
  public async refreshTools(context: string): Promise<void> {
    this.toolCache.delete(context);
    await this.loadToolsForContext(context);
  }

  /**
   * Get available MCP servers
   */
  public getAvailableMCPServers(): string[] {
    return Array.from(this.mcpClients.keys());
  }

  /**
   * Check if an MCP server is connected
   */
  public isMCPConnected(mcpKey: string): boolean {
    return this.mcpClients.has(mcpKey);
  }
}