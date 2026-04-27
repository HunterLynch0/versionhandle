#!/bin/bash

mkdir -p "$HOME/.versionhandle/bin"

curl -L -o "$HOME/.versionhandle/versionhandle.jar" \
https://github.com/YOUR_USERNAME/versionhandle/releases/download/v1.0.0/versionhandle.jar

cat > "$HOME/.versionhandle/bin/vh" << 'EOF'
#!/bin/bash
java -jar "$HOME/.versionhandle/versionhandle.jar" "$@"
EOF

chmod +x "$HOME/.versionhandle/bin/vh"

# Detect shell config file
if [ -n "$ZSH_VERSION" ]; then
    CONFIG="$HOME/.zshrc"
elif [ -n "$BASH_VERSION" ]; then
    CONFIG="$HOME/.bashrc"
else
    CONFIG="$HOME/.profile"
fi

echo 'export PATH="$HOME/.versionhandle/bin:$PATH"' >> "$CONFIG"

echo "Installed vh."
echo "Run this to activate:"
echo "source $CONFIG"