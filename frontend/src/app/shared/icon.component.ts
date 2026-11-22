import { Component, Input } from '@angular/core';
import { LucideAngularModule, Activity, Box, List, Minus, Zap, ArrowRight, ArrowUpRight, BookOpen, Check, ChevronDown, ChevronRight, CircleAlert, Columns3, Database, Eye, EyeOff, FlaskConical, Folder, GitBranch, Info, KeyRound, Layers3, Link2, LoaderCircle, LockKeyhole, Network, Plus, RefreshCw, Search, ShieldCheck, Table2, TerminalSquare, X } from 'lucide-angular';

const icons = {Activity,Box,List,Minus,Zap,ArrowRight,ArrowUpRight,BookOpen,Check,ChevronDown,ChevronRight,CircleAlert,Columns3,Database,Eye,EyeOff,FlaskConical,Folder,GitBranch,Info,KeyRound,Layers3,Link2,LoaderCircle,LockKeyhole,Network,Plus,RefreshCw,Search,ShieldCheck,Table2,TerminalSquare,X};
@Component({selector:'app-icon', imports:[LucideAngularModule], template:'<lucide-icon [img]="image" [size]="size" aria-hidden="true" />', styles:':host{display:inline-flex;flex-shrink:0;vertical-align:middle}lucide-icon{display:inline-flex}'})
export class IconComponent {
  @Input() name: keyof typeof icons = 'Database';
  @Input() size = 16;
  get image() { return icons[this.name]; }
}
