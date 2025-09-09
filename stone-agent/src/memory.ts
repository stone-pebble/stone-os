/**
 * Memory Client for Stone Agent
 * 
 * Manages persistent memory and context for conversations
 */
export class MemoryClient {
  private interactions: any[] = [];
  private userPreferences: Map<string, any> = new Map();

  constructor() {
    // Initialize memory system
    this.loadFromDisk();
  }

  /**
   * Store an interaction in memory
   */
  async storeInteraction(data: {
    input: string;
    context: string;
    timestamp: Date;
    app?: string;
    response?: string;
  }): Promise<void> {
    this.interactions.push(data);
    
    // Keep only last 1000 interactions in memory
    if (this.interactions.length > 1000) {
      this.interactions = this.interactions.slice(-1000);
    }

    await this.saveToDisk();
  }

  /**
   * Get relevant context for a query
   */
  async getRelevantContext(query: string): Promise<any> {
    // Simple relevance: get last 5 interactions from same app/context
    const recent = this.interactions.slice(-10);
    
    // Get user preferences
    const preferences = Object.fromEntries(this.userPreferences);

    return {
      recentInteractions: recent,
      preferences: preferences,
    };
  }

  /**
   * Store a user preference
   */
  async storePreference(key: string, value: any): Promise<void> {
    this.userPreferences.set(key, value);
    await this.saveToDisk();
  }

  /**
   * Get daily activity for reflection
   */
  async getDailyActivity(date?: Date): Promise<any[]> {
    const targetDate = date || new Date();
    const startOfDay = new Date(targetDate);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(targetDate);
    endOfDay.setHours(23, 59, 59, 999);

    return this.interactions.filter(i => {
      const timestamp = new Date(i.timestamp);
      return timestamp >= startOfDay && timestamp <= endOfDay;
    });
  }

  /**
   * Clear all memory (privacy feature)
   */
  async clearMemory(): Promise<void> {
    this.interactions = [];
    this.userPreferences.clear();
    await this.saveToDisk();
  }

  /**
   * Clear specific date range
   */
  async clearDateRange(startDate: Date, endDate: Date): Promise<void> {
    this.interactions = this.interactions.filter(i => {
      const timestamp = new Date(i.timestamp);
      return timestamp < startDate || timestamp > endDate;
    });
    await this.saveToDisk();
  }

  /**
   * Save memory to disk
   */
  private async saveToDisk(): Promise<void> {
    // In production, this would save to device storage
    // For now, using in-memory storage
    console.log('Memory saved (in-memory for now)');
  }

  /**
   * Load memory from disk
   */
  private async loadFromDisk(): Promise<void> {
    // In production, this would load from device storage
    // For now, starting with empty memory
    console.log('Memory loaded (empty for now)');
  }
}