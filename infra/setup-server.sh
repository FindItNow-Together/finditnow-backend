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

echo "Adding user to docker group..."
sudo usermod -aG docker $USER

echo "Setting up Firewall (UFW)..."
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo "Installing Certbot (for SSL/HTTPS)..."
sudo snap install core; sudo snap refresh core
sudo snap install --classic certbot
sudo ln -s /snap/bin/certbot /usr/bin/certbot || true

echo "Creating application directory..."
mkdir -p ~/finditnow-backend
cd ~/finditnow-backend

echo "Setup complete!"
echo "--------------------------------------------------------"
echo "1. Configure your .env file in ~/finditnow-backend/.env"
echo "2. Set up GitHub secrets for CI/CD."
echo "3. To get your SSL certificate after DNS is ready, run:"
echo "   sudo certbot certonly --standalone -d your-domain.duckdns.org"
echo "--------------------------------------------------------"
