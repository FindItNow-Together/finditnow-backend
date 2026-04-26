#!/bin/bash

# --- FindItNow Backend Server Setup Script ---
# Run this on a fresh Ubuntu 22.04+ VPS

set -e

echo "Updating system..."
sudo apt update && sudo apt upgrade -y

echo "Installing Docker..."
sudo apt install -y ca-certificates curl gnupg lsb-release
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "Setting up Firewall (UFW)..."
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo "Creating application directory..."
mkdir -p ~/finditnow-backend
cd ~/finditnow-backend

echo "Setup complete! Please ensure you have configured your .env file in ~/finditnow-backend/.env"
echo "You also need to provide GitHub secrets (SERVER_HOST, SERVER_USER, SERVER_SSH_KEY) for CI/CD to work."
