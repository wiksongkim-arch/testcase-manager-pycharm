/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: 'node',
  roots: ['<rootDir>/dist-test'],
  testMatch: ['**/__tests__/**/*.test.js'],
  moduleNameMapper: {
    '^@testcase-manager/shared$': '<rootDir>/../shared/dist/index.js',
  },
};
