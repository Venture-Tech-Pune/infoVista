#!/bin/bash

# INFOVISTA Backend Setup Script

echo "========================================"
echo "🚀 INFOVISTA Backend Setup"
echo "========================================"

# Create .env file
if [ ! -f .env ]; then
    echo "📝 Creating .env file..."
    cp .env.example .env
    echo "✅ .env file created. Please edit it with your configuration."
else
    echo "⚠️  .env file already exists"
fi

# Create uploads directory
if [ ! -d "uploads" ]; then
    echo "📁 Creating uploads directory..."
    mkdir uploads
    echo "✅ uploads directory created"
else
    echo "⚠️  uploads directory already exists"
fi

# Install dependencies
echo ""
echo "📦 Installing dependencies..."
npm install

echo ""
echo "========================================"
echo "✅ Setup Complete!"
echo "========================================"
echo ""
echo "⚡ Next steps:"
echo "1. Edit .env file and add your MongoDB URI and API keys"
echo "2. Make sure MongoDB is running"
echo "3. Run: npm run dev"
echo ""
