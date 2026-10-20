import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { functionalUpdate } from '@tanstack/react-table';
import { fetchProducts } from '../api.js';
import ProductsTable from '../components/ProductsTable.jsx';

export default function ProductsPage() {
  const [searchInput, setSearchInput] = useState('');
  const [view, setView] = useState({
    query: '', pagination: { pageIndex: 0, pageSize: 20 }, sorting: [],
  });

  useEffect(() => {
    const timer = setTimeout(() => {
      setView(current => current.query === searchInput.trim() ? current : {
        ...current, query: searchInput.trim(), pagination: { ...current.pagination, pageIndex: 0 },
      });
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const products = useQuery({
    queryKey: ['products', view],
    queryFn: ({ signal }) => fetchProducts({ ...view, signal }),
    placeholderData: keepPreviousData,
    refetchInterval: 5000,
    staleTime: 3000,
  });
  const { data, isPending, isFetching, isPlaceholderData, error, dataUpdatedAt } = products;

  function handlePaginationChange(updater) {
    setView(current => ({
      ...current, pagination: functionalUpdate(updater, current.pagination),
    }));
  }

  function handleSortingChange(updater) {
    setView(current => ({
      ...current, sorting: functionalUpdate(updater, current.sorting),
      pagination: { ...current.pagination, pageIndex: 0 },
    }));
  }

  function clearSearch() {
    setSearchInput('');
    setView(current => ({ ...current, query: '', pagination: { ...current.pagination, pageIndex: 0 } }));
  }

  return (
    <main className="catalog">
      <header className="page-header">
        <div className="heading"><span className="brand-mark" aria-hidden="true">PC</span><div>
          <p className="eyebrow">PRODUCT CATALOG</p>
          <h1>A closer look at your products.</h1>
        </div></div>
        <span className="live-badge"><span aria-hidden="true">●</span> Live prices · every 5s</span>
      </header>

      <section className="panel" aria-label="Product catalog">
        <div className="toolbar">
          <div className="search-control" role="search">
            <label htmlFor="product-search">Search products</label>
            <input id="product-search" type="search" placeholder="Try blue jacket or city lamp…"
              value={searchInput} onChange={event => setSearchInput(event.target.value)}
              aria-describedby="search-help" />
            <p id="search-help">Search product details and attributes, not SKU or price. Try <code>jacket -blue</code>.</p>
          </div>
          <button className="primary-button" type="button" disabled={isFetching} onClick={() => products.refetch()}>
            {isFetching ? 'Refreshing…' : 'Refresh prices'}
          </button>
        </div>

        <div className="table-summary">
          <div><strong>{data ? data.page.totalElements.toLocaleString() : '—'}</strong> matching products
            {searchInput && <button className="text-button" type="button" onClick={clearSearch}>Clear search</button>}
          </div>
          <div className="summary-actions">
            <span className="muted">{isPlaceholderData ? 'Updating results…' : dataUpdatedAt
              ? `Last refreshed ${new Date(dataUpdatedAt).toLocaleTimeString()}` : 'Waiting for products…'}</span>
            <button className="text-button" type="button" disabled={!view.sorting.length} onClick={() => handleSortingChange([])}>
              Reset sort
            </button>
          </div>
        </div>

        {error && <div className="error-notice" role="alert">
          {error.message} {data && 'Showing the last received results.'}
        </div>}

        <ProductsTable
          data={data}
          pagination={view.pagination}
          sorting={view.sorting}
          onPaginationChange={handlePaginationChange}
          onSortingChange={handleSortingChange}
          isPending={isPending}
          isFetching={isFetching}
          isPlaceholderData={isPlaceholderData}
          hasError={Boolean(error)}
        />
      </section>
      <p className="page-note">Search and sorting run on the server, across all matching products.
        Without a selected sort, searches use relevance and browsing uses SKU order.</p>
    </main>
  );
}
