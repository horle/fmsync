SELECT 
    MAX(ArachneForeignKey), TableName
FROM
    ceramalexEntityManagement
GROUP BY TableName
