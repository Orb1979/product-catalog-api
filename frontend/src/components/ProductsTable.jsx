import { flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table';

const emptyRows = [];
const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'short', timeStyle: 'medium' });
const columns = [
  { accessorKey: 'sku', header: 'SKU' },
  { accessorKey: 'name', header: 'Product' },
  { accessorKey: 'brand', header: 'Brand' },
  { accessorKey: 'category', header: 'Category' },
  {
    accessorKey: 'description', header: 'Description',
    cell: ({ getValue }) => <span className="truncated" title={getValue() || ''}>{getValue() || '—'}</span>,
  },
  {
    accessorKey: 'attributes', header: 'Attributes',
    cell: ({ getValue }) => {
      const text = Object.entries(getValue() || {}).map(([key, value]) =>
        `${key}: ${typeof value === 'object' ? JSON.stringify(value) : value}`).join(', ');
      return <span className="truncated attributes" title={text}>{text || '—'}</span>;
    },
  },
  {
    accessorKey: 'price', header: 'Price',
    cell: ({ getValue, row }) => getValue() == null
      ? <span className="muted">Not priced yet</span>
      : <span className="price">{new Intl.NumberFormat(undefined, row.original.currency
        ? { style: 'currency', currency: row.original.currency }
        : { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(getValue())}</span>,
  },
  { accessorKey: 'currency', header: 'Currency' },
  {
    accessorKey: 'priceUpdatedAt', header: 'Price updated',
    cell: ({ getValue }) => getValue()
      ? <time dateTime={getValue()}>{dateFormat.format(new Date(getValue()))}</time> : '—',
  },
];

export default function ProductsTable({
  data, pagination, sorting, onPaginationChange, onSortingChange,
  isPending, isFetching, isPlaceholderData, hasError,
}) {
  const table = useReactTable({
    data: data?.content ?? emptyRows,
    columns,
    defaultColumn: { cell: ({ getValue }) => getValue() ?? '—' },
    getCoreRowModel: getCoreRowModel(),
    getRowId: row => row.sku,
    manualPagination: true,
    manualSorting: true,
    enableMultiSort: false,
    sortDescFirst: false,
    pageCount: data?.page.totalPages ?? 0,
    state: { pagination, sorting },
    onPaginationChange,
    onSortingChange,
  });

  return (
    <>
      <div className="table-scroll" aria-busy={isFetching}>
        <table>
          <caption className="visually-hidden">Products. Use the column buttons to sort all matching products.</caption>
          <thead>{table.getHeaderGroups().map(group => <tr key={group.id}>
            {group.headers.map(header => <th key={header.id} scope="col"
              aria-sort={header.column.getIsSorted() === 'asc' ? 'ascending'
                : header.column.getIsSorted() === 'desc' ? 'descending' : 'none'}>
              <button type="button" onClick={header.column.getToggleSortingHandler()}
                aria-label={`Sort by ${header.column.columnDef.header}`}>
                {flexRender(header.column.columnDef.header, header.getContext())}
                <span className="sort-arrow" aria-hidden="true">{header.column.getIsSorted() === 'asc' ? '↑'
                  : header.column.getIsSorted() === 'desc' ? '↓' : '↕'}</span>
              </button>
            </th>)}
          </tr>)}</thead>
          <tbody>
            {table.getRowModel().rows.map(row => <tr key={row.id}>
              {row.getVisibleCells().map(cell => <td key={cell.id}>
                {flexRender(cell.column.columnDef.cell, cell.getContext())}
              </td>)}
            </tr>)}
            {!table.getRowModel().rows.length && <tr><td colSpan={columns.length} className="empty-state">
              {isPending ? 'Loading products…' : hasError ? 'Products could not be loaded. Try refreshing.'
                : 'No products found. Try another search or clear the search field.'}
            </td></tr>}
          </tbody>
        </table>
      </div>

      <footer className="pagination">
        <label>Rows per page <select value={pagination.pageSize} onChange={event => onPaginationChange({
          pageIndex: 0, pageSize: Number(event.target.value),
        })}>
          {[20, 50, 100].map(size => <option key={size} value={size}>{size}</option>)}
        </select></label>
        <div className="page-controls">
          <span>{data?.page.totalPages ? `Page ${pagination.pageIndex + 1} of ${data.page.totalPages}` : 'No pages'}</span>
          <button type="button" disabled={!table.getCanPreviousPage() || isPlaceholderData || isPending}
            onClick={() => table.previousPage()}>Previous</button>
          <button type="button" disabled={!table.getCanNextPage() || isPlaceholderData || isPending}
            onClick={() => table.nextPage()}>Next</button>
        </div>
      </footer>
    </>
  );
}
