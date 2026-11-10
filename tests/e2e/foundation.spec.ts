import { expect, test } from '../../frontend/testing/playwright';

test('both applications connect through their own API proxy and handle unknown routes', async ({ page, context }) => {
  await page.goto('http://127.0.0.1:5173');
  await expect(page.getByRole('heading', { name: '学员端基础工程已就绪' })).toBeVisible();
  await expect(page.getByRole('status')).toContainText('服务连接正常');
  await page.goto('http://127.0.0.1:5174');
  await expect(page.getByRole('heading', { name: '管理端基础工程已就绪' })).toBeVisible();
  await expect(page.getByRole('status')).toContainText('服务连接正常');
  const cookies = await context.cookies();
  expect(cookies.find((cookie) => cookie.name === 'PORTAL_SESSION')?.path).toBe('/api/v1/portal');
  expect(cookies.find((cookie) => cookie.name === 'ADMIN_SESSION')?.path).toBe('/api/v1/admin');
  await page.goto('http://127.0.0.1:5174/not-a-page');
  await expect(page.getByRole('heading', { name: '没有找到这个页面' })).toBeVisible();
});
