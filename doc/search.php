<html><head>

<link rel="stylesheet" type="text/css" href="https://categoricaldata.net/css/simple.css" /></head><body><h1>Search (case/space sensitive)</h1>

<div>
  <form action="search.php" method="get">
       <input type="text" name="text" value=<?php echo "\"" . $_GET["text"] . "\"" ; ?> > 
              <input type="submit" name="submit" value="Search">
              
              <br>
    </form>

</div>


<?php

$string = $_GET["text"];


if (strpos(file_get_contents('../help.html'), $string) !== false) {
        echo "<a href=\"../help.html\">CQL Manual</a><br/>";
}
    
$dir = new DirectoryIterator('.');
foreach ($dir as $file) {
    if ($file == 'search.php') {
        continue;   
    }
    if ($file == 'help.html') {
        continue;   
    }
    if ($file == 'options.html') {
        continue;   
    }
    if ($file == 'examples.html') {
        continue;   
    }
    if ($file == 'syntax.html') {
        continue;   
    }
    $content = file_get_contents($file->getPathname());
    
    if (strpos($content, $string) !== false) {
        echo "<a href=\"" . $file . "\">" . $file . "</a><br/>";
    }
}

?>



</body></html>