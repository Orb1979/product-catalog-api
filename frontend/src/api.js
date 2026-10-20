export async function fetchProducts({ query, pagination, sorting, signal }) {
  const params = new URLSearchParams({
    page: String(pagination.pageIndex),
    size: String(pagination.pageSize),
  });
  if (query.trim()) params.set('q', query.trim());
  if (sorting.length) {
    params.set('sort', sorting[0].id);
    params.set('direction', sorting[0].desc ? 'desc' : 'asc');
  }

  const response = await fetch(`/api/products?${params}`, { signal });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || `Could not load products (HTTP ${response.status}).`);
  }
  return response.json();
}
