# Set-ExecutionPolicy RemoteSigned
for($row=1; $row -le 4; $row++){
	for($column=1; $column -le 4; $column++){
		Start-Process java -ArgumentList 'basePack.Main', $row, $column -NoNewWindow
	}	
}
