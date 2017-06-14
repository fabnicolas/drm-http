<?php
$path1="./uploads/";
$path2="./tmp/";
$arr=scandir("./uploads");
for ( $i=0;$i<count($arr);$i++){
	echo "File: ".$arr[$i]."<br/>";
	$file_name = $arr[$i];
if(strpos($arr[$i],".tmp")){
	// Raising this value may increase performance
	$buffer_size = 4096; // read 4kb at a time
	$out_file_name = str_replace('.tmp', '', $file_name); 

	// Open our files (in binary mode)
	$file = gzopen($path1.$file_name, 'rb');
	$out_file = fopen($path2.$out_file_name, 'wb'); 

	// Keep repeating until the end of the input file
	while (!gzeof($file)) {
		// Read buffer-size bytes
		// Both fwrite and gzread and binary-safe
		fwrite($out_file, gzread($file, $buffer_size));
	}

	// Files are done, close files
	fclose($out_file);
	gzclose($file);
}

//This input should be from somewhere else, hard-coded in this example
if(false){
$file_name = '2013-07-16.dump.gz';

// Raising this value may increase performance
$buffer_size = 4096; // read 4kb at a time
$out_file_name = str_replace('.gz', '', $file_name); 

// Open our files (in binary mode)
$file = gzopen($file_name, 'rb');
$out_file = fopen($out_file_name, 'wb'); 

// Keep repeating until the end of the input file
while (!gzeof($file)) {
    // Read buffer-size bytes
    // Both fwrite and gzread and binary-safe
    fwrite($out_file, gzread($file, $buffer_size));
}

// Files are done, close files
fclose($out_file);
gzclose($file);

$dfile=fopen($path2.$out_file_name,'rb');
$fsize = filesize($out_file_name); 
$contents = fread($handle, $fsize); 
$byteArray = unpack("N*",$contents); 
print_r($byteArray); 
for($n = 0; $n < 16; $n++)
{ 
    echo $byteArray [$n].'<br/>'; 
}
}
}
?>