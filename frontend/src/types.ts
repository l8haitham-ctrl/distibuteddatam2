export interface Source {
  id: string
  engine: string
  host: string
  port: number
  database: string
  username: string
  default_schema?: string
}

export interface ConnectorInfo {
  engine: string; name: string; default_port: number; default_schema: string;
  available: boolean; capabilities: string[];
}

export interface ConnectionConfig extends Omit<Source, 'id'|'default_schema'> {
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
  character_length?: number
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
