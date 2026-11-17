import type { Column, TableRef } from '../types'

export const tableKey = (table: TableRef) => JSON.stringify([table.schema_name, table.table_name])

export function formatType(column: Column): string {
  const type = column.data_type.toLowerCase()
  if (['decimal', 'numeric'].includes(type)) return `${type}(${column.precision}, ${column.scale})`
  if (['varchar', 'char', 'nvarchar', 'nchar', 'binary', 'varbinary'].includes(type)) {
    const size = column.max_length_bytes === -1 ? 'max' : column.max_length_bytes / (type.startsWith('n') ? 2 : 1)
    return `${type}(${size})`
  }
  if (['datetime2', 'datetimeoffset', 'time'].includes(type)) return `${type}(${column.scale})`
  return type
}

export function groupTables(tables: TableRef[], search: string): Map<string, TableRef[]> {
  const result = new Map<string, TableRef[]>()
  const query = search.trim().toLocaleLowerCase('fr')
  for (const table of tables) {
    if (!`${table.schema_name}.${table.table_name}`.toLocaleLowerCase('fr').includes(query)) continue
    result.set(table.schema_name, [...(result.get(table.schema_name) ?? []), table])
  }
  return result
}
