javac -d .\cloudSpanningTreeServer\out cloudSpanningTreeServer\src\basePack\*.java

# Set-ExecutionPolicy RemoteSigned
for($row=1; $row -le 4; $row++){
	for($column=1; $column -le 4; $column++){
		Start-Process -NoNewWindow java -ArgumentList "-cp", ".\\cloudSpanningTreeServer\\out", "basePack.Main", $row, $column
	}	
}
