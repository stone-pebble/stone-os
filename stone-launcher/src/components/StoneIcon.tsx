import React from 'react';
import { View, StyleSheet, Animated } from 'react-native';
import Svg, { Path, Circle } from 'react-native-svg';

interface StoneIconProps {
  size?: number;
  color?: string;
  glowing?: boolean;
}

const StoneIcon: React.FC<StoneIconProps> = ({ 
  size = 32, 
  color = '#FFFFFF',
  glowing = false 
}) => {
  const glowAnimation = React.useRef(new Animated.Value(0.3)).current;

  React.useEffect(() => {
    if (glowing) {
      Animated.loop(
        Animated.sequence([
          Animated.timing(glowAnimation, {
            toValue: 1,
            duration: 1000,
            useNativeDriver: true,
          }),
          Animated.timing(glowAnimation, {
            toValue: 0.3,
            duration: 1000,
            useNativeDriver: true,
          }),
        ])
      ).start();
    } else {
      glowAnimation.setValue(0.3);
    }
  }, [glowing, glowAnimation]);

  return (
    <View style={styles.container}>
      {glowing && (
        <Animated.View
          style={[
            styles.glowEffect,
            {
              opacity: glowAnimation,
              width: size * 2,
              height: size * 2,
            },
          ]}
        />
      )}
      <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
        {/* Simple stone/pebble shape */}
        <Path
          d="M12 3C7.5 3 4 6.5 4 11C4 15.5 7.5 19 12 19C16.5 19 20 15.5 20 11C20 6.5 16.5 3 12 3Z"
          stroke={color}
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* Inner detail for depth */}
        <Path
          d="M8 9C8.5 8 10 7 12 7C14 7 15.5 8 16 9C16.5 10 15 11 12 11C9 11 7.5 10 8 9Z"
          stroke={color}
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          opacity={0.6}
        />
      </Svg>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  glowEffect: {
    position: 'absolute',
    backgroundColor: '#FFFFFF',
    borderRadius: 100,
    shadowColor: '#FFFFFF',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.5,
    shadowRadius: 20,
    elevation: 10,
  },
});

export default StoneIcon;