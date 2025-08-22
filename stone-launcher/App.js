/**
 * StoneOS Launcher
 * Minimalist AI-augmented Android launcher
 */

import React, {useState} from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  Dimensions,
} from 'react-native';
import HomeScreen from './src/screens/HomeScreen';
import StoneIcon from './src/components/StoneIcon';
import StoneChat from './src/components/StoneChat';

const {height} = Dimensions.get('window');

const App = () => {
  const [chatVisible, setChatVisible] = useState(false);
  const [currentScreen, setCurrentScreen] = useState('home');

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#f0f0f0" />
      
      {/* Main Screen Content */}
      <View style={styles.mainContent}>
        {currentScreen === 'home' && <HomeScreen onAppSelect={setCurrentScreen} />}
        {currentScreen === 'listen' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>LISTEN</Text>
            <Text style={styles.appSubtitle}>Spotify integration coming soon</Text>
          </View>
        )}
        {currentScreen === 'go' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>GO</Text>
            <Text style={styles.appSubtitle}>Maps integration coming soon</Text>
          </View>
        )}
        {currentScreen === 'ask' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>ASK</Text>
            <Text style={styles.appSubtitle}>Perplexity integration coming soon</Text>
          </View>
        )}
        {currentScreen === 'connect' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>CONNECT</Text>
            <Text style={styles.appSubtitle}>Communications hub coming soon</Text>
          </View>
        )}
        {currentScreen === 'plan' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>PLAN</Text>
            <Text style={styles.appSubtitle}>Calendar integration coming soon</Text>
          </View>
        )}
        {currentScreen === 'think' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>THINK</Text>
            <Text style={styles.appSubtitle}>Notion integration coming soon</Text>
          </View>
        )}
        {currentScreen === 'set' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>SET</Text>
            <Text style={styles.appSubtitle}>Settings & 2FA coming soon</Text>
          </View>
        )}
        {currentScreen === 'tick' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>TICK</Text>
            <Text style={styles.appSubtitle}>Clock & timers coming soon</Text>
          </View>
        )}
        {currentScreen === 'fund' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>FUND</Text>
            <Text style={styles.appSubtitle}>Wallet & banking coming soon</Text>
          </View>
        )}
        {currentScreen === 'reflect' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>REFLECT</Text>
            <Text style={styles.appSubtitle}>AI journal coming soon</Text>
          </View>
        )}
        {currentScreen === 'task' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>TASK</Text>
            <Text style={styles.appSubtitle}>MCP discovery coming soon</Text>
          </View>
        )}
        {currentScreen === 'look' && (
          <View style={styles.appScreen}>
            <Text style={styles.appTitle}>LOOK</Text>
            <Text style={styles.appSubtitle}>Digital library coming soon</Text>
          </View>
        )}
      </View>

      {/* Stone Icon - Always visible at bottom */}
      <StoneIcon 
        onPress={() => setChatVisible(!chatVisible)}
        style={styles.stoneIcon}
      />

      {/* Chat Interface - Slides up from bottom */}
      {chatVisible && (
        <StoneChat 
          visible={chatVisible}
          onClose={() => setChatVisible(false)}
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f0f0f0',
  },
  mainContent: {
    flex: 1,
  },
  appScreen: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
  },
  appTitle: {
    fontSize: 32,
    fontWeight: '300',
    color: '#333',
    marginBottom: 10,
  },
  appSubtitle: {
    fontSize: 16,
    color: '#666',
  },
  stoneIcon: {
    position: 'absolute',
    bottom: 20,
    alignSelf: 'center',
  },
});

export default App;