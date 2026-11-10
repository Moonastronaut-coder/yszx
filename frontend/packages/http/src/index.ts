import type { ApiErrorDetail, ApiResponse, CsrfView, Realm } from '@yunshu/shared';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly requestId: string | null,
    public readonly detail: ApiErrorDetail | null = null,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  json?: unknown;
  body?: FormData;
  headers?: HeadersInit;
  signal?: AbortSignal;
}

/** 工厂只共享能力；每个应用拥有独立令牌和请求实例，不存储到 localStorage。 */
export function createApiClient(realm: Realm, fetcher: typeof fetch = globalThis.fetch) {
  const base = `/api/v1/${realm}`;
  let csrf: CsrfView | undefined;
  let loading: Promise<CsrfView> | undefined;

  async function send<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const url = new URL(base + path, 'http://api.invalid');
    if (!path.startsWith('/') || path.startsWith('//') || !url.pathname.startsWith(base + '/')) {
      throw new Error('请求路径必须位于当前应用 API 范围内');
    }
    const headers = new Headers(options.headers);
    headers.set('Accept', 'application/json');
    if (options.json !== undefined) headers.set('Content-Type', 'application/json');
    let response: Response;
    try {
      response = await fetcher(url.pathname + url.search, {
        method: options.method ?? 'GET',
        credentials: 'same-origin',
        redirect: 'error',
        headers,
        body: options.json === undefined ? options.body : JSON.stringify(options.json),
        signal: options.signal,
      });
    } catch (error) {
      if (options.signal?.aborted) throw error;
      throw new ApiError(0, 'NETWORK_ERROR', '暂时无法连接服务', null);
    }
    let envelope: ApiResponse<T>;
    try {
      envelope = (await response.json()) as ApiResponse<T>;
      if (typeof envelope.code !== 'string' || typeof envelope.request_id !== 'string')
        throw new Error();
    } catch {
      throw new ApiError(
        response.status,
        'INVALID_RESPONSE',
        '服务响应格式不正确',
        response.headers.get('X-Request-ID'),
      );
    }
    if (!response.ok || envelope.code !== 'OK') {
      if (response.status === 401 || envelope.code === 'CSRF_INVALID') csrf = undefined;
      throw new ApiError(
        response.status,
        envelope.code,
        envelope.message,
        envelope.request_id,
        envelope.error,
      );
    }
    return envelope.data;
  }

  async function refreshCsrf(): Promise<CsrfView> {
    loading ??= send<CsrfView>('/auth/csrf')
      .then((value) => {
        if (!value.token || value.header_name !== 'X-CSRF-TOKEN')
          throw new Error('CSRF 响应格式不正确');
        csrf = value;
        return value;
      })
      .finally(() => {
        loading = undefined;
      });
    return loading;
  }

  return {
    refreshCsrf,
    resetCsrf() {
      csrf = undefined;
    },
    async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
      const method = options.method ?? 'GET';
      const headers = new Headers(options.headers);
      if (method !== 'GET') {
        const token = csrf ?? (await refreshCsrf());
        headers.set(token.header_name, token.token);
      }
      // 不自动重放写请求，避免重复创建、作答或扣次。
      return send<T>(path, { ...options, headers });
    },
  };
}
