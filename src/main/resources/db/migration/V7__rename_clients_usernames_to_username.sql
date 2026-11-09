-- V7: Corrige o nome da coluna do nome do cliente
--
-- A V1 criou a coluna como "usernames", no plural, mas ela guarda um unico
-- nome por cliente. A entidade Client ja chamava o campo de "username" e
-- mapeava para a coluna antiga; agora coluna e campo tem o mesmo nome.
-- RENAME preserva os dados e a restricao NOT NULL.
ALTER TABLE clients RENAME COLUMN usernames TO username;
