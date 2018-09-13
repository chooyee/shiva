@ECHO OFF
SET SvcName=shiva

sc queryex "%SvcName%" | find "STATE" | find /v "RUNNING" > NUL && (
	
	echo %SvcName% is not running 
	echo START %SvcName%

	NET START "%SvcName%" > NUL || (
		echo "%SvcName%" wont start
		EXIT /B 1
	)

	echo "%SvcName%" is started 
	EXIT /B 0
) || (
	echo "%SvcName%" is running
	EXIT /B 0 
)

