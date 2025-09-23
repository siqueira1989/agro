SELECT 
    ac.idalimentoclassificacao,
    a.nomeproduto,
    a.variedadealimento,
    c.idclassificacao,
    c.classificacao
FROM alimentoclassificacao ac
JOIN alimento a ON ac.idproduto = a.idproduto
JOIN classificacao c ON ac.idclassificacao = c.idclassificacao;