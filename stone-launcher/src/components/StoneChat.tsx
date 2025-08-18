import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Animated,
  PanResponder,
  Dimensions,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import StoneIcon from './StoneIcon';

const { height } = Dimensions.get('window');
const CHAT_HEIGHT = height * 0.33;
const SWIPE_THRESHOLD = 50;

interface StoneChatProps {
  onSendMessage: (message: string) => void;
  messages: Array<{ role: 'user' | 'assistant'; content: string }>;
}

const StoneChat: React.FC<StoneChatProps> = ({ onSendMessage, messages }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isVoiceMode, setIsVoiceMode] = useState(false);
  const [inputText, setInputText] = useState('');
  const [isAISpeaking, setIsAISpeaking] = useState(false);
  
  const translateY = useRef(new Animated.Value(CHAT_HEIGHT)).current;
  const scrollViewRef = useRef<ScrollView>(null);

  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (_, gestureState) => {
        return Math.abs(gestureState.dy) > 5;
      },
      onPanResponderMove: (_, gestureState) => {
        if (gestureState.dy > 0 && isOpen) {
          translateY.setValue(Math.min(gestureState.dy, CHAT_HEIGHT));
        } else if (gestureState.dy < 0 && !isOpen) {
          translateY.setValue(CHAT_HEIGHT + gestureState.dy);
        }
      },
      onPanResponderRelease: (_, gestureState) => {
        if (gestureState.dy > SWIPE_THRESHOLD && isOpen) {
          closeChat();
        } else if (gestureState.dy < -SWIPE_THRESHOLD && !isOpen) {
          openChat();
        } else {
          // Snap back
          Animated.spring(translateY, {
            toValue: isOpen ? 0 : CHAT_HEIGHT,
            useNativeDriver: true,
          }).start();
        }
      },
    })
  ).current;

  const openChat = () => {
    setIsOpen(true);
    Animated.spring(translateY, {
      toValue: 0,
      useNativeDriver: true,
      tension: 50,
      friction: 8,
    }).start();
  };

  const closeChat = () => {
    setIsOpen(false);
    setIsVoiceMode(false);
    Animated.spring(translateY, {
      toValue: CHAT_HEIGHT,
      useNativeDriver: true,
      tension: 50,
      friction: 8,
    }).start();
  };

  const toggleVoiceMode = () => {
    setIsVoiceMode(!isVoiceMode);
    if (!isVoiceMode) {
      // Starting voice mode - would connect to LiveKit here
      console.log('Starting voice mode...');
    }
  };

  const sendMessage = () => {
    if (inputText.trim()) {
      onSendMessage(inputText);
      setInputText('');
    }
  };

  return (
    <>
      {/* Stone Icon Button - Always Visible */}
      <TouchableOpacity
        style={styles.stoneButton}
        onPress={openChat}
        activeOpacity={0.8}
      >
        <StoneIcon size={28} glowing={isAISpeaking} />
      </TouchableOpacity>

      {/* Chat Interface */}
      <Animated.View
        style={[
          styles.chatContainer,
          {
            transform: [{ translateY }],
          },
        ]}
        {...panResponder.panHandlers}
      >
        <View style={styles.handle} />
        
        {isVoiceMode ? (
          // Voice Mode - No transcription shown
          <View style={styles.voiceMode}>
            <StoneIcon size={64} glowing={isAISpeaking} />
            <Text style={styles.voiceModeText}>
              {isAISpeaking ? 'listening...' : 'tap to speak'}
            </Text>
            <TouchableOpacity
              style={styles.modeToggle}
              onPress={toggleVoiceMode}
            >
              <Text style={styles.modeToggleText}>switch to text</Text>
            </TouchableOpacity>
          </View>
        ) : (
          // Text Mode
          <KeyboardAvoidingView
            style={styles.textMode}
            behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          >
            <ScrollView
              ref={scrollViewRef}
              style={styles.messageList}
              contentContainerStyle={styles.messageListContent}
              onContentSizeChange={() => scrollViewRef.current?.scrollToEnd()}
            >
              {messages.map((msg, index) => (
                <View
                  key={index}
                  style={[
                    styles.message,
                    msg.role === 'user' ? styles.userMessage : styles.assistantMessage,
                  ]}
                >
                  <Text style={styles.messageText}>{msg.content}</Text>
                </View>
              ))}
            </ScrollView>
            
            <View style={styles.inputContainer}>
              <TextInput
                style={styles.textInput}
                value={inputText}
                onChangeText={setInputText}
                placeholder="type a message..."
                placeholderTextColor="#666666"
                returnKeyType="send"
                onSubmitEditing={sendMessage}
              />
              <TouchableOpacity
                style={styles.voiceToggle}
                onPress={toggleVoiceMode}
              >
                <Text style={styles.voiceToggleIcon}>🎤</Text>
              </TouchableOpacity>
            </View>
          </KeyboardAvoidingView>
        )}
      </Animated.View>
    </>
  );
};

const styles = StyleSheet.create({
  stoneButton: {
    position: 'absolute',
    bottom: 20,
    alignSelf: 'center',
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#1A1A1A',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  chatContainer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: CHAT_HEIGHT,
    backgroundColor: '#0A0A0A',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    borderTopWidth: 1,
    borderTopColor: '#1A1A1A',
  },
  handle: {
    width: 40,
    height: 4,
    backgroundColor: '#333333',
    borderRadius: 2,
    alignSelf: 'center',
    marginTop: 8,
    marginBottom: 8,
  },
  voiceMode: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  voiceModeText: {
    color: '#666666',
    fontSize: 16,
    marginTop: 20,
    fontFamily: 'serif',
  },
  textMode: {
    flex: 1,
  },
  messageList: {
    flex: 1,
    paddingHorizontal: 16,
  },
  messageListContent: {
    paddingVertical: 8,
  },
  message: {
    marginVertical: 4,
    padding: 12,
    borderRadius: 16,
    maxWidth: '80%',
  },
  userMessage: {
    alignSelf: 'flex-end',
    backgroundColor: '#1A1A1A',
  },
  assistantMessage: {
    alignSelf: 'flex-start',
    backgroundColor: '#0F0F0F',
    borderWidth: 1,
    borderColor: '#1A1A1A',
  },
  messageText: {
    color: '#FFFFFF',
    fontSize: 14,
    lineHeight: 20,
  },
  inputContainer: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderTopWidth: 1,
    borderTopColor: '#1A1A1A',
  },
  textInput: {
    flex: 1,
    backgroundColor: '#1A1A1A',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
    color: '#FFFFFF',
    fontSize: 14,
  },
  voiceToggle: {
    marginLeft: 8,
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#1A1A1A',
    justifyContent: 'center',
    alignItems: 'center',
  },
  voiceToggleIcon: {
    fontSize: 20,
  },
  modeToggle: {
    position: 'absolute',
    bottom: 40,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  modeToggleText: {
    color: '#666666',
    fontSize: 14,
  },
});

export default StoneChat;