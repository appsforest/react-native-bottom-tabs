module.exports = {
  root: true,
  extends: ['@react-native', 'prettier'],
  plugins: ['prettier'],
  rules: {
    'react/react-in-jsx-scope': 'off',
  },
  ignorePatterns: [
    '**/lib/**',
    '**/dist/**',
    '**/node_modules/**',
    'expo-env.d.ts',
  ],
};
