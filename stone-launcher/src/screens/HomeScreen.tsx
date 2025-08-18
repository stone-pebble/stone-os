import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Dimensions,
} from 'react-native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';

const { width } = Dimensions.get('window');

interface HomeScreenProps {
  navigation: any;
}

const HomeScreen: React.FC<HomeScreenProps> = ({ navigation }) => {
  const apps = [
    { name: 'tick', screen: 'Tick' },
    { name: 'task', screen: 'Task' },
    { name: 'set', screen: 'Set' },
    { name: 'listen', screen: 'Listen' },
    { name: 'ask', screen: 'Ask' },
    { name: 'look', screen: 'Look' },
    { name: 'plan', screen: 'Plan' },
    { name: 'think', screen: 'Think' },
    { name: 'reflect', screen: 'Reflect' },
    { name: 'connect', screen: 'Connect' },
    { name: 'go', screen: 'Go' },
    { name: 'fund', screen: 'Fund' },
  ];

  const handleAppPress = (screen: string) => {
    navigation.navigate(screen);
  };

  return (
    <GestureHandlerRootView style={styles.container}>
      <View style={styles.grid}>
        {apps.map((app) => (
          <TouchableOpacity
            key={app.name}
            style={styles.appButton}
            onPress={() => handleAppPress(app.screen)}
            activeOpacity={0.7}
          >
            <Text style={styles.appText}>{app.name}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </GestureHandlerRootView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
    paddingTop: 60,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 32,
    paddingTop: 40,
  },
  appButton: {
    width: (width - 64) / 3,
    height: 100,
    justifyContent: 'center',
    alignItems: 'center',
  },
  appText: {
    color: '#FFFFFF',
    fontSize: 20,
    fontFamily: 'serif',
  },
});

export default HomeScreen;