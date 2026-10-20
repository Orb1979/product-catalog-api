import assert from 'node:assert/strict';
import test from 'node:test';
import { fetchProducts } from './api.js';

const pagination = { pageIndex: 2, pageSize: 20 };

test('sends search, pagination, sorting and cancellation to the server', async t => {
  const signal = new AbortController().signal;
  const response = { content: [], page: { totalElements: 0 } };
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    const params = new URL(url, 'http://localhost').searchParams;
    assert.equal(params.get('q'), 'blue jacket');
    assert.equal(params.get('page'), '2');
    assert.equal(params.get('size'), '20');
    assert.equal(params.get('sort'), 'price');
    assert.equal(params.get('direction'), 'desc');
    assert.equal(options.signal, signal);
    return Response.json(response);
  });

  assert.deepEqual(await fetchProducts({ query: '  blue jacket  ', pagination,
    sorting: [{ id: 'price', desc: true }], signal }), response);
});

test('omits blank search and sorting so backend defaults are preserved', async t => {
  t.mock.method(globalThis, 'fetch', async url => {
    const params = new URL(url, 'http://localhost').searchParams;
    assert.equal(params.has('q'), false);
    assert.equal(params.has('sort'), false);
    assert.equal(params.has('direction'), false);
    return Response.json({ content: [] });
  });

  await fetchProducts({ query: '   ', pagination, sorting: [] });
});

test('shows the API problem detail instead of accepting a failed response', async t => {
  t.mock.method(globalThis, 'fetch', async () =>
    Response.json({ detail: 'Unsupported product sort field.' }, { status: 400 }));

  await assert.rejects(fetchProducts({ query: '', pagination, sorting: [] }),
    /Unsupported product sort field/);
});
