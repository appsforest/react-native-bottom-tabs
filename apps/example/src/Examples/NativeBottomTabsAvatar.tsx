import { Article } from '../Screens/Article';
import { Albums } from '../Screens/Albums';
import { Contacts } from '../Screens/Contacts';
import { Chat } from '../Screens/Chat';
import { createNativeBottomTabNavigator } from '@appsforest/react-navigation';
import { Platform } from 'react-native';
import { useState } from 'react';

const Tab = createNativeBottomTabNavigator();

function NativeBottomTabsAvatar() {
  const [label, setLabel] = useState('Article');
  return (
    <Tab.Navigator
      labeled={true}
      hapticFeedbackEnabled={false}
      layoutDirection="ltr"
      tabBarInactiveTintColor="#C57B57"
      tabBarActiveTintColor="#F7DBA7"
      tabBarStyle={{
        backgroundColor: '#1E2D2F',
      }}
      rippleColor="#F7DBA7"
      tabLabelStyle={{
        fontFamily: 'Avenir',
        fontSize: 15,
      }}
      activeIndicatorColor="#041F1E"
      screenListeners={{
        tabLongPress: (data) => {
          console.log(
            `${Platform.OS}: Long press detected on tab with key ${data.target} at the navigator level.`
          );
        },
      }}
    >
      <Tab.Screen
        name="Article"
        component={Article}
        listeners={{
          tabLongPress: (data) => {
            console.log(
              `${Platform.OS}: Long press detected on tab with key ${data.target} at the screen level.`
            );
            setLabel('New Article');
          },
        }}
        options={{
          tabBarButtonTestID: 'articleTestID',
          tabBarBadge: '10',
          tabBarLabel: label,
          tabBarIcon: ({ focused }) =>
            focused
              ? require('../../assets/icons/person_dark.png')
              : require('../../assets/icons/article_dark.png'),
        }}
      />
      <Tab.Screen
        name="Albums"
        component={Albums}
        options={{
          tabBarIcon: () => require('../../assets/icons/grid_dark.png'),
        }}
      />
      <Tab.Screen
        name="Chat"
        component={Chat}
        listeners={{
          tabPress: () => {
            console.log('Chat tab pressed');
          },
        }}
        options={{
          tabBarIcon: () => ({
            uri: require('../../assets/icons/chat_dark.png'),
            width: 20,
            height: 20,
          }),
          tabBarActiveTintColor: 'white',
        }}
      />
      <Tab.Screen
        name="Profile"
        component={Contacts}
        options={{
          tabBarIcon: () => ({
            avatar: {
              uri: 'https://t3.ftcdn.net/jpg/06/01/50/96/360_F_601509638_jDwIDvlnryPRhXPsBeW1nXv90pdlbykC.jpg',
              size: 20,
              initials: 'JD',
              strokeColor: '#4c98e4',
              strokeWidth: 2,
              strokeGap: 2,
              backgroundColor: '#767676',
            },
          }),
        }}
      />
    </Tab.Navigator>
  );
}

export default NativeBottomTabsAvatar;
