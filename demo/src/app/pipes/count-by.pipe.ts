// ══════════════════════════════════════════════════════════════════════════════
// FILE 1: src/app/pipes/count-by.pipe.ts
// ══════════════════════════════════════════════════════════════════════════════
/*
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'countBy', standalone: false })
export class CountByPipe implements PipeTransform {
  transform(items: any[], key: string, value: any): number {
    if (!items) return 0;
    return items.filter(i => i[key] === value).length;
  }
}
*/

// ══════════════════════════════════════════════════════════════════════════════
// FILE 2: src/app/pipes/count-by.pipe.ts  (actual file — remove the /* */ above)
// ══════════════════════════════════════════════════════════════════════════════
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'countBy', standalone: false })
export class CountByPipe implements PipeTransform {
  transform(items: any[], key: string, value: any): number {
    if (!items) return 0;
    return items.filter(i => i[key] === value).length;
  }
}