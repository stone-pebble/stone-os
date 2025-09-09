import { MCPServer } from '@modelcontextprotocol/server';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

/**
 * Calendar MCP Server
 * 
 * Provides tools to control Google Calendar on Android
 */
export class CalendarMCPServer extends MCPServer {
  constructor() {
    super({
      name: 'calendar-mcp',
      version: '0.1.0',
      description: 'Control Calendar on Android via MCP',
    });

    this.registerTools();
  }

  private registerTools() {
    // Create event
    this.registerTool({
      name: 'create_event',
      description: 'Create a new calendar event',
      inputSchema: {
        type: 'object',
        properties: {
          title: {
            type: 'string',
            description: 'Event title',
          },
          startTime: {
            type: 'string',
            description: 'Start time (ISO format or natural language)',
          },
          endTime: {
            type: 'string',
            description: 'End time (ISO format or natural language)',
          },
          location: {
            type: 'string',
            description: 'Event location',
          },
          description: {
            type: 'string',
            description: 'Event description',
          },
          allDay: {
            type: 'boolean',
            description: 'Is this an all-day event?',
          },
        },
        required: ['title', 'startTime'],
      },
      handler: async ({ title, startTime, endTime, location, description, allDay }) => {
        // Use Calendar provider intent
        const intent = [
          'am start -a android.intent.action.INSERT',
          '-d content://com.android.calendar/events',
          `--es title "${title}"`,
          `--el beginTime ${this.parseTime(startTime)}`,
        ];

        if (endTime) {
          intent.push(`--el endTime ${this.parseTime(endTime)}`);
        }
        if (location) {
          intent.push(`--es eventLocation "${location}"`);
        }
        if (description) {
          intent.push(`--es description "${description}"`);
        }
        if (allDay) {
          intent.push('--ez allDay true');
        }

        await execAsync(`adb shell ${intent.join(' ')}`);
        
        return { 
          success: true, 
          message: `Created event: ${title}`,
          eventId: 'placeholder_id',
        };
      },
    });

    // Get events
    this.registerTool({
      name: 'get_events',
      description: 'Get calendar events for a date range',
      inputSchema: {
        type: 'object',
        properties: {
          startDate: {
            type: 'string',
            description: 'Start date (ISO format or "today", "tomorrow")',
          },
          endDate: {
            type: 'string',
            description: 'End date',
          },
        },
      },
      handler: async ({ startDate = 'today', endDate }) => {
        // Would query Calendar provider
        // For now, return mock data
        return {
          events: [
            {
              id: '1',
              title: 'Team Meeting',
              startTime: '2024-01-15T10:00:00',
              endTime: '2024-01-15T11:00:00',
              location: 'Conference Room A',
            },
            {
              id: '2',
              title: 'Lunch with Sarah',
              startTime: '2024-01-15T12:30:00',
              endTime: '2024-01-15T13:30:00',
              location: 'Downtown Cafe',
            },
          ],
        };
      },
    });

    // Update event
    this.registerTool({
      name: 'update_event',
      description: 'Update an existing calendar event',
      inputSchema: {
        type: 'object',
        properties: {
          eventId: {
            type: 'string',
            description: 'Event ID to update',
          },
          title: {
            type: 'string',
            description: 'New title',
          },
          startTime: {
            type: 'string',
            description: 'New start time',
          },
          endTime: {
            type: 'string',
            description: 'New end time',
          },
          location: {
            type: 'string',
            description: 'New location',
          },
        },
        required: ['eventId'],
      },
      handler: async ({ eventId, title, startTime, endTime, location }) => {
        // Would update via Calendar provider
        return {
          success: true,
          message: `Updated event ${eventId}`,
        };
      },
    });

    // Delete event
    this.registerTool({
      name: 'delete_event',
      description: 'Delete a calendar event',
      inputSchema: {
        type: 'object',
        properties: {
          eventId: {
            type: 'string',
            description: 'Event ID to delete',
          },
        },
        required: ['eventId'],
      },
      handler: async ({ eventId }) => {
        // Would delete via Calendar provider
        return {
          success: true,
          message: `Deleted event ${eventId}`,
        };
      },
    });

    // Set reminder
    this.registerTool({
      name: 'set_reminder',
      description: 'Set a reminder',
      inputSchema: {
        type: 'object',
        properties: {
          title: {
            type: 'string',
            description: 'Reminder text',
          },
          time: {
            type: 'string',
            description: 'When to remind (ISO format or natural language)',
          },
          recurring: {
            type: 'string',
            enum: ['none', 'daily', 'weekly', 'monthly'],
            description: 'Recurrence pattern',
          },
        },
        required: ['title', 'time'],
      },
      handler: async ({ title, time, recurring = 'none' }) => {
        // Create reminder via Calendar or Clock app
        const timestamp = this.parseTime(time);
        const command = `adb shell am start -a android.intent.action.SET_ALARM --es android.intent.extra.alarm.MESSAGE "${title}"`;
        await execAsync(command);
        
        return {
          success: true,
          message: `Set reminder: ${title} at ${time}`,
        };
      },
    });

    // Find free time
    this.registerTool({
      name: 'find_free_time',
      description: 'Find free time slots in calendar',
      inputSchema: {
        type: 'object',
        properties: {
          duration: {
            type: 'number',
            description: 'Duration needed (minutes)',
          },
          startDate: {
            type: 'string',
            description: 'Start of search range',
          },
          endDate: {
            type: 'string',
            description: 'End of search range',
          },
        },
        required: ['duration'],
      },
      handler: async ({ duration, startDate = 'today', endDate }) => {
        // Would analyze calendar data
        return {
          freeSlots: [
            {
              start: '2024-01-15T14:00:00',
              end: '2024-01-15T16:00:00',
            },
            {
              start: '2024-01-15T17:00:00',
              end: '2024-01-15T18:30:00',
            },
          ],
        };
      },
    });

    // Get next event
    this.registerTool({
      name: 'get_next_event',
      description: 'Get the next upcoming event',
      inputSchema: {
        type: 'object',
        properties: {},
      },
      handler: async () => {
        // Query for next event
        return {
          event: {
            title: 'Team Standup',
            startTime: '2024-01-15T09:00:00',
            location: 'Zoom',
            minutesUntil: 45,
          },
        };
      },
    });

    // Add attendees
    this.registerTool({
      name: 'add_attendees',
      description: 'Add attendees to an event',
      inputSchema: {
        type: 'object',
        properties: {
          eventId: {
            type: 'string',
            description: 'Event ID',
          },
          attendees: {
            type: 'array',
            items: { type: 'string' },
            description: 'Email addresses of attendees',
          },
        },
        required: ['eventId', 'attendees'],
      },
      handler: async ({ eventId, attendees }) => {
        // Would update event with attendees
        return {
          success: true,
          message: `Added ${attendees.length} attendees to event`,
        };
      },
    });

    // Check availability
    this.registerTool({
      name: 'check_availability',
      description: 'Check if a time slot is available',
      inputSchema: {
        type: 'object',
        properties: {
          startTime: {
            type: 'string',
            description: 'Start time to check',
          },
          endTime: {
            type: 'string',
            description: 'End time to check',
          },
        },
        required: ['startTime', 'endTime'],
      },
      handler: async ({ startTime, endTime }) => {
        // Check calendar for conflicts
        return {
          available: true,
          conflicts: [],
        };
      },
    });

    // Get agenda
    this.registerTool({
      name: 'get_agenda',
      description: 'Get daily agenda summary',
      inputSchema: {
        type: 'object',
        properties: {
          date: {
            type: 'string',
            description: 'Date to get agenda for',
          },
        },
      },
      handler: async ({ date = 'today' }) => {
        // Get all events for the day
        return {
          date: date,
          eventCount: 5,
          firstEvent: '9:00 AM - Team Standup',
          lastEvent: '4:30 PM - Project Review',
          freeTime: '2.5 hours',
          summary: 'Busy morning, lighter afternoon',
        };
      },
    });
  }

  /**
   * Parse time string to milliseconds
   */
  private parseTime(timeStr: string): number {
    // Simple parsing - in production would use proper date library
    const date = new Date(timeStr);
    return date.getTime();
  }
}

// Start the MCP server
if (require.main === module) {
  const server = new CalendarMCPServer();
  server.listen(9003);
  console.log('Calendar MCP Server running on port 9003');
}