import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Dimensions,
  TouchableWithoutFeedback,
} from 'react-native';
import { GestureHandlerRootView, PanGestureHandler } from 'react-native-gesture-handler';
import Markdown from 'react-native-markdown-display';

const { width, height } = Dimensions.get('window');

interface UnlockScreenProps {
  navigation: any;
}

const UnlockScreen: React.FC<UnlockScreenProps> = ({ navigation }) => {
  // This content would come from the Stone agent's notification aggregation
  const unlockContent = `
you've been working on stone os for 3 hours.
good progress on the react native port.

---

• michael liu meeting request sent
• livekit sdk integration pending
• ui components migrating well

---

sarah called twice about tomorrow's demo.
might want to call her back.

---

*72° and sunny - perfect for that walk*
  `.trim();

  const handleSwipeUp = () => {
    navigation.navigate('Home');
  };

  const handleSwipeRight = () => {
    navigation.navigate('Camera');
  };

  const handleTap = () => {
    navigation.navigate('Stone');
  };

  return (
    <GestureHandlerRootView style={styles.container}>
      <TouchableWithoutFeedback onPress={handleTap}>
        <View style={styles.content}>
          <ScrollView 
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
          >
            <Markdown style={markdownStyles}>
              {unlockContent}
            </Markdown>
          </ScrollView>
          
          <Text style={styles.swipeHint}>swipe up to unlock</Text>
        </View>
      </TouchableWithoutFeedback>
    </GestureHandlerRootView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 32,
    paddingVertical: 64,
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  swipeHint: {
    position: 'absolute',
    bottom: 48,
    alignSelf: 'center',
    color: '#666666',
    fontSize: 14,
    fontFamily: 'serif',
  },
});

const markdownStyles = {
  body: {
    color: '#FFFFFF',
    fontSize: 16,
    lineHeight: 24,
    fontFamily: 'serif',
  },
  heading1: {
    color: '#FFFFFF',
    fontSize: 24,
    marginBottom: 16,
  },
  paragraph: {
    marginBottom: 16,
    color: '#CCCCCC',
  },
  hr: {
    backgroundColor: '#333333',
    height: 1,
    marginVertical: 24,
  },
  bullet_list: {
    marginLeft: 0,
  },
  bullet_list_icon: {
    color: '#666666',
  },
  em: {
    color: '#888888',
    fontStyle: 'italic',
  },
};

export default UnlockScreen;