/** BIGINT 标识和计数以十进制字符串传输，不转换为 JavaScript number。 */
export type Id = string;
export type Count = string;
export type Realm = 'portal' | 'admin';

export interface ApiErrorDetail {
  field_errors: Array<{ field: string; code: string; message: string }>;
  details: Record<string, unknown>;
}

export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  request_id: string;
  error: ApiErrorDetail | null;
}

export interface Page<T> {
  items: T[];
  page: number;
  page_size: number;
  total: Count;
  total_pages: Count;
}

export interface CsrfView {
  token: string;
  header_name: string;
}
