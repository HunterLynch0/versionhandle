#!/bin/bash

mkdir -p "$HOME/.versionhandle/bin"

cp target/versionhandle.jar "$HOME/.versionhandle/versionhandle.jar"

cat > "$HOME/.versionhandle/bin/vh" << 'EOF'
#!/bin/bash
java -jar "$HOME/.versionhandle/versionhandle.jar" "$@"
EOF

chmod +x "$HOME/.versionhandle/bin/vh"

echo "Installed vh."
echo "Add this to your ~/.zshrc:"
echo 'export PATH="$HOME/.versionhandle/bin:$PATH"'