CREATE CONSTRAINT unique_vm_id ON (v:VM) ASSERT v.vmId IS UNIQUE;
CREATE CONSTRAINT unique_method_id ON (m:Method) ASSERT m.methodId IS UNIQUE;

LOAD CSV WITH HEADERS FROM 'file:///reports/csv_call_tree_vm.csv' AS row
MERGE (v:VM {vmId: row.Id, name: row.Name})
RETURN count(v);

LOAD CSV WITH HEADERS FROM 'file:///reports/csv_call_tree_methods.csv' AS row
MERGE (m:Method {methodId: row.Id, name: row.Name, type: row.Type, parameters: row.Parameters, return: row.Return, display: row.Display})
RETURN count(m);

LOAD CSV WITH HEADERS FROM 'file:///reports/csv_call_tree_virtual_methods.csv' AS row
MERGE (m:Method {methodId: row.Id, name: row.Name, type: row.Type, parameters: row.Parameters, return: row.Return, display: row.Display})
RETURN count(m);

LOAD CSV WITH HEADERS FROM 'file:///reports/csv_call_tree_entry_points.csv' AS row
MATCH (m:Method {methodId: row.Id})
MATCH (v:VM {vmId: '0'})
MERGE (v)-[:ENTRY]->(m)
RETURN count(*);

LOAD CSV WITH HEADERS FROM 'file:///reports/csv_call_tree_direct_edges.csv' AS row
MATCH (m1:Method {methodId: row.StartId})
MATCH (m2:Method {methodId: row.EndId})
MERGE (m1)-[:DIRECT {bci: row.BytecodeIndexes}]->(m2)
RETURN count(*);

LOAD CSV WITH HEADERS FROM 'file:///reports/csv_call_tree_override_by_edges.csv' AS row
MATCH (m1:Method {methodId: row.StartId})
MATCH (m2:Method {methodId: row.EndId})
MERGE (m1)-[:OVERRIDEN_BY]->(m2)
RETURN count(*);

LOAD CSV WITH HEADERS FROM 'file:///reports/csv_call_tree_virtual_edges.csv' AS row
MATCH (m1:Method {methodId: row.StartId})
MATCH (m2:Method {methodId: row.EndId})
MERGE (m1)-[:VIRTUAL {bci: row.BytecodeIndexes}]->(m2)
RETURN count(*);