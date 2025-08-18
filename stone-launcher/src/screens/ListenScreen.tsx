import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  Text,
} from 'react-native';
import { WebView } from 'react-native-webview';
import StoneChat from '../components/StoneChat';

interface ListenScreenProps {
  navigation: any;
}

const ListenScreen: React.FC<ListenScreenProps> = ({ navigation }) => {
  const [messages, setMessages] = useState<Array<{ role: 'user' | 'assistant'; content: string }>>([]);

  const handleSendMessage = async (message: string) => {
    // Add user message
    setMessages(prev => [...prev, { role: 'user', content: message }]);
    
    // This would connect to the Stone agent with Spotify MCP tools
    // For now, simulate a response
    setTimeout(() => {
      if (message.toLowerCase().includes('play')) {
        setMessages(prev => [...prev, { 
          role: 'assistant', 
          content: 'Playing your music...' 
        }]);
      } else if (message.toLowerCase().includes('skip')) {
        setMessages(prev => [...prev, { 
          role: 'assistant', 
          content: 'Skipping to next track...' 
        }]);
      } else {
        setMessages(prev => [...prev, { 
          role: 'assistant', 
          content: 'I can help you control Spotify. Try saying "play jazz" or "skip this song".' 
        }]);
      }
    }, 500);
  };

  return (
    <View style={styles.container}>
      {/* This would be replaced with actual Spotify app embedding using root access */}
      {/* For now, showing WebView as placeholder */}
      <View style={styles.appContainer}>
        <WebView
          source={{ uri: 'https://open.spotify.com' }}
          style={styles.webview}
          // Apply grayscale filter
          injectedCSS={`
            * {
              filter: grayscale(100%) !important;
              -webkit-filter: grayscale(100%) !important;
            }
          `}
        />
      </View>
      
      {/* Overlay message when native app embedding is ready */}
      <View style={styles.placeholder}>
        <Text style={styles.placeholderText}>
          Spotify app will be embedded here{'\n'}
          (requires root access)
        </Text>
      </View>

      {/* Stone Chat Interface */}
      <StoneChat 
        messages={messages}
        onSendMessage={handleSendMessage}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  appContainer: {
    flex: 1,
  },
  webview: {
    flex: 1,
    backgroundColor: '#000000',
  },
  placeholder: {
    position: 'absolute',
    top: '50%',
    left: 0,
    right: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    padding: 20,
    alignItems: 'center',
  },
  placeholderText: {
    color: '#666666',
    fontSize: 16,
    textAlign: 'center',
    fontFamily: 'serif',
  },
});

export default ListenScreen;