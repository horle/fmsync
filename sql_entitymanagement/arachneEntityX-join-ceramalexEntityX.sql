SELECT PS_MainAbstractID,Level1,Level2,Level3,Level4,Level5,Level6, PS_DatierungID
FROM xmorphologyx
left JOIN mainabstract ON mainabstract.PS_MainAbstractID = xmorphologyx.FS_MainAbstractID
LEFT join datierung on datierung.FS_MorphologyID = xmorphologyx.FS_MorphologyID
WHERE (Level2 LIKE 'Amphora%' OR (Level1='Table ware' AND (Level3 LIKE 'Gempeler%' OR Level3 LIKE 'Consp.%' OR Level4 LIKE 'Magdalensberg%')))

UNION
SELECT PS_MainAbstractID,Level1,Level2,Level3,Level4,Level5,Level6, PS_DatierungID
FROM mainabstract
left JOIN fabric ON mainabstract.FS_FabricID = fabric.PS_FabricID
left join fabricdescription on fabric.PS_FabricID=fabricdescription.FS_FabricID
left join xmorphologyx on xmorphologyx.FS_MainAbstractID=mainabstract.PS_MainAbstractID
left join datierung on datierung.FS_FabricDescriptionID = fabricdescription.PS_FabricDescriptionID
WHERE (Level2 LIKE 'Amphora%' OR (Level1='Table ware' AND (Level3 LIKE 'Gempeler%' OR Level3 LIKE 'Consp.%' OR Level4 LIKE 'Magdalensberg%')))