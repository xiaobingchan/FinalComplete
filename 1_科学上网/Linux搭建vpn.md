AWS免费云主机的申请过程和搭建Shadowsocks过程


选择"实例 New"

选择系统映像，这里我选择的是Ubuntu 20.04

默认用户名是ubuntu, 密码是  “密钥对”的PEM文件
这是官方的连接说明


这里以ubuntu为例，其他的centos等网上的解决方案也很多，安装SHADOWSOCKS

# 首先备份源列表
sudo cp /etc/apt/sources.list /etc/apt/sources.list_backup
 
# 打开sources.list文件
sudo gedit /etc/apt/sources.list
 
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
sudo apt-get update

# 第二条命令会弹出确认提示，输入Y
sudo apt-get install python-pip git -y

# pip安装ssr
sudo pip install git+https://github.com/shadowsocks/shadowsocks.git@master

# 编辑ssr配置
sudo vim /etc/shadowsocks.json
{"server":"0.0.0.0","local_address":"127.0.0.1","local_port":1080,"port_password":{"9000":"password0","9001":"password1","9002":"password2","9003":"password3","9004":"password4"},"timeout":300,"method":"aes-256-cfb","fast_open":false}
 
# 开放端口
sudo ufw allow 9000
sudo ufw allow 9001
sudo ufw allow 9002
sudo ufw allow 9003
sudo ufw allow 9004
...
 
开启服务
 
sudo ssserver -c /etc/shadowsocks.json -d start
 
查看确认启动
 
/bin/ps axu | grep ssserver | grep -v greproot 
 
 
 
关闭服务
 
ssserver -c /etc/shadowsocks.json -d stop
 
查看防火墙状态
 
ubuntu@ip-172-31-44-56:~$ sudo ufw status
 

SSR 配置 , 下载地址：Windows SSR：https://github.com/HMBSbige/ShadowsocksR-Windows/releases


安装完成之后，启动，任务栏会有一个小飞机的图标

先点击启动系统代理


右键->服务器->编辑服务器

加密：aes-256-cfb
协议：无
混淆：无

成功！ 访问：https://www.google.com/
 

安装加速器Google TCP BBR

sudo wget --no-check-certificate https://github.com/teddysun/across/raw/master/bbr.sh
sudo  chmod +x bbr.sh
sudo  ./bbr.sh
 