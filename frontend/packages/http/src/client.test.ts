import { describe, expect, it, vi } from 'vitest';
import { ApiError, createApiClient } from './index';

function ok(data: unknown) {
  return new Response(
    JSON.stringify({ code: 'OK', message: '成功', data, request_id: 'test-request', error: null }),
  );
}

describe('realm HTTP clients', () => {
  it('keeps tokens separate and sends cookies with each write', async () => {
    const fetcher = vi.fn<typeof fetch>().mockImplementation(async (url) => {
      return String(url).endsWith('/csrf')
        ? ok({
            token: String(url).includes('/portal/') ? 'portal-token' : 'admin-token',
            header_name: 'X-CSRF-TOKEN',
          })
        : ok(null);
    });
    const portal = createApiClient('portal', fetcher);
    const admin = createApiClient('admin', fetcher);
    await portal.request('/test', { method: 'POST', json: {} });
    await admin.request('/test', { method: 'POST', json: {} });
    expect(new Headers(fetcher.mock.calls[1]?.[1]?.headers).get('X-CSRF-TOKEN')).toBe(
      'portal-token',
    );
    expect(new Headers(fetcher.mock.calls[3]?.[1]?.headers).get('X-CSRF-TOKEN')).toBe(
      'admin-token',
    );
    expect(fetcher.mock.calls[1]?.[1]?.credentials).toBe('same-origin');
  });

  it('does not retry a rejected write and preserves the request id', async () => {
    const fetcher = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(ok({ token: 'token', header_name: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: 'CSRF_INVALID',
            message: '拒绝',
            data: null,
            request_id: 'trace-1',
            error: null,
          }),
          { status: 403 },
        ),
      );
    await expect(
      createApiClient('portal', fetcher).request('/test', { method: 'POST' }),
    ).rejects.toMatchObject({ status: 403, code: 'CSRF_INVALID', requestId: 'trace-1' });
    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  it('rejects paths that escape the current realm', async () => {
    const fetcher = vi.fn<typeof fetch>();
    await expect(createApiClient('portal', fetcher).request('/../admin/me')).rejects.toThrow();
    expect(fetcher).not.toHaveBeenCalled();
  });

  it('reports malformed gateway responses without treating them as success', async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(
      new Response('<html>error</html>', {
        status: 502,
        headers: { 'X-Request-ID': 'gateway-1' },
      }),
    );
    await expect(createApiClient('admin', fetcher).request('/test')).rejects.toBeInstanceOf(
      ApiError,
    );
  });
});
