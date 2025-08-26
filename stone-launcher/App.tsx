import React from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  ScrollView,
  StatusBar,
} from 'react-native';

const App = () => {
  const apps = [
    { name: 'LISTEN', screen: 'Listen' },
    { name: 'GO', screen: 'Go' },
    { name: 'ASK', screen: 'Ask' },
    { name: 'CONNECT', screen: 'Connect' },
    { name: 'PLAN', screen: 'Plan' },
    { name: 'THINK', screen: 'Think' },
    { name: 'SET', screen: 'Set' },
    { name: 'TICK', screen: 'Tick' },
    { name: 'FUND', screen: 'Fund' },
    { name: 'REFLECT', screen: 'Reflect' },
    { name: 'TASK', screen: 'Task' },
    { name: 'LOOK', screen: 'Look' },
  ];

  return (
    <>
      <StatusBar barStyle="dark-content" backgroundColor="#f5f5f5" />
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>stone os</Text>
        </View>
        
        <ScrollView contentContainerStyle={styles.appGrid}>
          {apps.map((app, index) => (
            <TouchableOpacity
              key={index}
              style={styles.appButton}
              onPress={() => console.log(`Opening ${app.name}`)}
            >
              <Text style={styles.appName}>{app.name}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
        
        <View style={styles.stoneBar}>
          <TouchableOpacity style={styles.stoneIcon}>
            <Text style={styles.stoneEmoji}>🗿</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    </>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  title: {
    fontSize: 24,
    fontWeight: '300',
    color: '#333',
    textAlign: 'center',
  },
  appGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    padding: 20,
    justifyContent: 'space-between',
  },
  appButton: {
    width: '30%',
    aspectRatio: 1,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 15,
  },
  appName: {
    fontSize: 12,
    fontWeight: '600',
    color: '#666',
    letterSpacing: 1,
  },
  stoneBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 60,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  stoneIcon: {
    width: 50,
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
  },
  stoneEmoji: {
    fontSize: 30,
  },
});

export default App;