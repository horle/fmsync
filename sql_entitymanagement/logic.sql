#insert into mainabstract (NULL,-1,123,123,123,12,"DIESISTEINTEST","a",NULL,NULL,"TEST","Felix")

SELECT 
    arachne.arachneentityidentification.ArachneEntityID, arachne.arachneentityidentification.TableName,
    ceramalex.ceramalexEntityManagement.ArachneEntityID, ceramalex.ceramalexEntityManagement.TableName
FROM
    arachne.arachneentityidentification
        JOIN
    ceramalex.ceramalexEntityManagement ON arachne.arachneentityidentification.ArachneEntityID = ceramalex.ceramalexEntityManagement.ArachneEntityID
WHERE
    arachne.arachneentityidentification.isDeleted = 1