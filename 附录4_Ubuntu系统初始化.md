# 开机必备

# 首先备份源列表
sudo cp /etc/apt/sources.list /etc/apt/sources.list_backup

# 打开sources.list文件
sudo vi /etc/apt/sources.list
# 编辑/etc/apt/sources.list文件, 在文件最前面添加阿里云镜像源：
deb http://mirrors.aliyun.com/ubuntu/ bionic main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-security main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-updates main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-proposed main restricted universe multiverse
deb http://mirrors.aliyun.com/ubuntu/ bionic-backports main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-security main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-updates main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-proposed main restricted universe multiverse
deb-src http://mirrors.aliyun.com/ubuntu/ bionic-backports main restricted universe multiverse
# 刷新列表
sudo apt-get update -y


########################
sudo apt-get update -y
sudo apt-get install openssh-client -y
sudo apt-get install telnet unzip wget net-tools -y

sudo apt-get install -y gcc g++
sudo apt-get install -y cmake make
sudo apt-get install -y git
sudo apt-get install -y python
sudo apt-get install -y python-pip

sudo apt-get install -y python3
sudo apt-get install -y python3-pip

# 安装vim
sudo apt-get purge vim-common -y
sudo apt-get update -y
sudo apt-get install vim -y
########################


# 开放SSH防火墙
########################
sudo ufw enable
sudo ufw allow 22
sudo ufw status
sudo ufw disable

# ubuntu apt卸载软件
sudo apt-get --purge remove <package>				# 删除软件及其配置文件
sudo apt-get autoremove <package>					# 删除没用的依赖包


# RDP 远程桌面连接Ubuntu
################################################
sudo apt-get install xrdp -y

# CentOS 搭建opencv+tensoflow python3 开发环境
yum install -y  gcc-c++ python3-devel
yum install -y  git
yum install -y  python3
yum install -y  libSM
yum install -y  libXrender
yum install -y  libXext
yum install -y  cmake make


# 安装 Docker
################################################
sudo apt-get purge  libcurl3-gnutls -y
sudo apt-get install curl -y
sudo apt-get install linux-image-generic-lts-xenial -y

sudo apt-get install docker.io

sudo mkdir -p /etc/docker
cat >/etc/docker/daemon.json <<EOF
{
  "registry-mirrors": ["https://ot7dvptd.mirror.aliyuncs.com"]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
################################################

docker run --runtime=nvidia -it --rm tensorflow/tensorflow:1.12.0-gpu-py3
docker pull tensorflow/tensorflow:1.14.0-gpu-py3