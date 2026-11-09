// Test setup and global mocks for frontend test runners (Vitest / Jest)
import '@testing-library/jest-dom/vitest';

// Mock localStorage if in node/jsdom environment
if (typeof window !== 'undefined') {
  window.matchMedia = window.matchMedia || function() {
    return {
      matches: false,
      addListener: function() {},
      removeListener: function() {},
      addEventListener: function() {},
      removeEventListener: function() {},
      dispatchEvent: function() { return false; },
    };
  };
}
