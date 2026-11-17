export interface Source {
  id: string
  engine: 'sqlserver'
  host: string
  port: number
  database: string
  username: string
}

export interface ConnectionConfig extends Omit<Source, 'id'> {
  password: string
  trust_server_certificate: boolean
}

export interface TableRef { schema_name: string; table_name: string }
export interface Column {
  name: string
  data_type: string
  position: number
  max_length_bytes: number
  precision: number
  scale: number
  nullable: boolean
  is_identity: boolean
  default_value: string | null
}
export interface PrimaryKey { constraint_name: string; column_name: string; position: number }
export interface ForeignKey extends PrimaryKey {
  referenced_schema: string
  referenced_table: string
  referenced_column: string
}
export interface TableDetails extends TableRef {
  columns: Column[]
  primary_key: PrimaryKey[]
  foreign_keys: ForeignKey[]
}
export interface Exploration { database: string; tables: TableDetails[] }
