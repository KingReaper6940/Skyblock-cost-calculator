import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

const inter = Inter({ subsets: ["latin"], variable: "--font-geist-sans" });

export const metadata: Metadata = {
  title: "SkyBlock Crafter | Cheapest Craft Cost Calculator",
  description: "Calculate the cheapest live craft cost for Hypixel SkyBlock items using realtime Bazaar data.",
  openGraph: {
    title: "SkyBlock Crafter",
    description: "Calculate the cheapest live craft cost for Hypixel SkyBlock items.",
    siteName: "SkyBlock Crafter",
  }
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark">
      <body className={`${inter.variable} min-h-screen flex flex-col font-sans`}>
        <Header />
        <main className="flex-1 flex flex-col">
          {children}
        </main>
        <Footer />
      </body>
    </html>
  );
}
