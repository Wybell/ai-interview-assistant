import { afterEach, describe, expect, it } from 'vitest';

import { clearSession, readSession, writeSession } from '@/utils/session';

describe('浏览器会话存储', () => {
  afterEach(() => {
    clearSession();
  });

  it('保存并读取当前认证会话', () => {
    writeSession({ token: 'test-token', username: 'candidate01' });

    expect(readSession()).toEqual({ token: 'test-token', username: 'candidate01' });
  });

  it('忽略不完整会话', () => {
    window.sessionStorage.setItem('ai-interview-assistant.session', '{"token":"only-token"}');

    expect(readSession()).toBeNull();
  });
});
