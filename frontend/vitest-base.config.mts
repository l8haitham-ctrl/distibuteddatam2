import { defineConfig } from 'vitest/config';

// Bound DOM/JIT workers alongside SQL Server, Trino and Spark in local Docker.
export default defineConfig({ test: { maxWorkers: 2 } });
