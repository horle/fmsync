SELECT 
    *
FROM
    arachneentityidentification
WHERE
    ForeignKey NOT IN (SELECT 
            arachneentityidentification.ForeignKey
        FROM
            ceramalexEntityManagement
                JOIN
            arachneentityidentification ON ceramalexEntityManagement.TableName = arachneentityidentification.TableName
                AND ceramalexEntityManagement.CeramalexForeignKey = arachneentityidentification.ForeignKey)