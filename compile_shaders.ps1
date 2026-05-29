$shaderDir = "src/main/resources/shaders"

# Find all .vert and .frag files
$shaders = Get-ChildItem -Path $shaderDir -Include *.vert, *.frag -Recurse

foreach ($shader in $shaders) {
    $output = "$($shader.FullName).spv"
    Write-Host "Compiling $($shader.Name) -> $($shader.Name).spv"
    glslc $shader.FullName -o $output
}

Write-Host "Done!"
