// Illustrative metadata for the separately labelled mockup. Never used as an API fallback.
import type { Column, Exploration, Source, TableDetails } from '../types'

export const mockSource: Source = { id: 'sqlserver', engine: 'sqlserver', host: 'sqlserver', port: 1433, database: 'BusinessDB', username: 'lake_reader' }
const column = (name: string, data_type: string, position: number, length = 4): Column => ({
  name, data_type, position, max_length_bytes: length, precision: data_type === 'decimal' ? 12 : 10,
  scale: data_type === 'decimal' ? 2 : 0, nullable: false, is_identity: false, default_value: null,
})
const table = (name: string, columns: Column[], parent?: [string, string]): TableDetails => ({
  schema_name: 'dbo', table_name: name, columns,
  primary_key: [{ constraint_name: `PK_${name}`, column_name: columns[0].name, position: 1 }],
  foreign_keys: parent ? [{ constraint_name: `FK_${name}_${parent[0]}`, column_name: parent[1], position: 1,
    referenced_schema: 'dbo', referenced_table: parent[0], referenced_column: parent[1] }] : [],
})
export const mockExploration: Exploration = { database: 'BusinessDB', tables: [
  table('Customer', [column('customer_id', 'int', 1), column('first_name', 'nvarchar', 2, 200), column('last_name', 'nvarchar', 3, 200),
    column('email', 'nvarchar', 4, 510), column('city', 'nvarchar', 5, 200), column('created_at', 'datetime2', 6, 6)]),
  table('Invoice', [column('invoice_id', 'int', 1), column('customer_id', 'int', 2), column('invoice_date', 'date', 3, 3),
    column('amount', 'decimal', 4, 9), column('status', 'varchar', 5, 20)], ['Customer', 'customer_id']),
  table('Payment', [column('payment_id', 'int', 1), column('invoice_id', 'int', 2), column('payment_date', 'date', 3, 3),
    column('amount', 'decimal', 4, 9), column('payment_method', 'varchar', 5, 30)], ['Invoice', 'invoice_id']),
] }
