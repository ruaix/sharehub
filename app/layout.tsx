import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "拼友 · 合租管理平台",
  description: "轻量、清晰的数字服务合租与成员管理平台。",
  icons: { icon: "/favicon.svg" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="zh-CN"><body>{children}</body></html>;
}
