import type { Metadata } from "next";
import Dashboard from "./Dashboard";

export const metadata: Metadata = {
  title: "拼友 · 合租管理平台",
  description: "轻量、清晰的数字服务合租与成员管理平台。",
};

export default function Home() {
  return <Dashboard />;
}
